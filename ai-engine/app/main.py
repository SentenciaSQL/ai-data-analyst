import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes.analyst import build_analyst_router
from app.api.routes.health import build_health_router
from app.api.routes.sql import router as sql_router
from app.configuration.settings import get_settings
from app.providers.factory import build_provider
from app.workflows.analyst_graph import build_graph

settings = get_settings()
provider = build_provider(settings)
graph = build_graph(provider)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings.assert_production_safe()
    logging.basicConfig(level=settings.log_level, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    yield


app = FastAPI(title="AI Data Analyst Engine", version="0.1.0", lifespan=lifespan)
app.include_router(build_health_router(provider))
app.include_router(build_analyst_router(provider, graph))
app.include_router(sql_router)
