from fastapi import APIRouter
from fastapi.responses import Response
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest

from app.providers.base import ChatProvider

router = APIRouter()


def build_health_router(provider: ChatProvider) -> APIRouter:
    @router.get("/health")
    def health() -> dict:
        return {
            "status": "ok",
            "service": "ai-data-analyst-engine",
            "provider": provider.name,
            "providerConfigured": provider.configured,
        }

    @router.get("/metrics")
    def metrics() -> Response:
        return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

    return router
