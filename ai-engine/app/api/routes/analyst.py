import asyncio
import logging
from collections.abc import AsyncIterator
from uuid import UUID

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from prometheus_client import Counter
from pydantic import BaseModel, ConfigDict, Field

from app.api.sse import sse
from app.providers.base import ChatProvider, ProviderError
from app.security.internal_auth import require_internal_key
from app.workflows.prompts import classify_intent, static_answer, system_prompt

logger = logging.getLogger(__name__)
ANALYST_REQUESTS = Counter(
    "ada_engine_analyst_requests_total",
    "Analyst requests accepted by the engine.",
)


class AnalystRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    message: str = Field(min_length=1, max_length=4000)
    tenant_id: UUID = Field(alias="tenantId")
    user_id: UUID = Field(alias="userId")


class AnalystResponse(BaseModel):
    intent: str
    answer: str


def build_analyst_router(provider: ChatProvider, graph) -> APIRouter:
    router = APIRouter(prefix="/internal/v1/analyst", dependencies=[Depends(require_internal_key)])

    @router.post("/invoke", response_model=AnalystResponse)
    def invoke(request: AnalystRequest) -> AnalystResponse:
        ANALYST_REQUESTS.inc()
        result = graph.invoke({"message": request.message})
        logger.info("analyst_invoke tenant=%s intent=%s", request.tenant_id, result.get("intent"))
        return AnalystResponse(intent=result["intent"], answer=result["answer"])

    @router.post("/stream")
    async def stream(request: AnalystRequest) -> StreamingResponse:
        async def events() -> AsyncIterator[str]:
            ANALYST_REQUESTS.inc()
            intent = classify_intent(request.message)
            logger.info("analyst_stream tenant=%s intent=%s", request.tenant_id, intent)
            yield sse("status", {"stage": "classify_intent"})
            yield sse("status", {"stage": "intent", "intent": intent})
            yield sse("status", {"stage": "compose_answer"})
            if provider.configured:
                try:
                    for token in provider.stream(system_prompt(intent), request.message):
                        yield sse("token", {"text": token})
                        await asyncio.sleep(0)
                except ProviderError:
                    logger.warning("provider stream failed tenant=%s", request.tenant_id)
                    yield sse("error", {"message": "The AI provider request failed."})
                    return
            else:
                result = await asyncio.to_thread(graph.invoke, {"message": request.message})
                async for piece in _chunks(result["answer"]):
                    yield sse("token", {"text": piece})
            yield sse("done", {"intent": intent})

        return StreamingResponse(events(), media_type="text/event-stream", headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        })

    return router


async def _chunks(text: str) -> AsyncIterator[str]:
    buffer: list[str] = []
    length = 0
    for word in text.split(" "):
        buffer.append(word)
        length += len(word) + 1
        if length >= 40:
            yield " ".join(buffer) + " "
            buffer = []
            length = 0
            await asyncio.sleep(0.02)
    if buffer:
        yield " ".join(buffer)
