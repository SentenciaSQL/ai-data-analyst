from fastapi import APIRouter, Depends
from pydantic import BaseModel, ConfigDict, Field

from app.security.internal_auth import require_internal_key
from app.sql_validation.validator import validate_sql

router = APIRouter(prefix="/internal/v1/sql", dependencies=[Depends(require_internal_key)])


class SqlValidateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    sql: str = Field(min_length=1, max_length=20000)
    engine: str = Field(min_length=1, max_length=40)
    allowed_tables: list[str] | None = Field(default=None, alias="allowedTables")


class SqlValidateResponse(BaseModel):
    accepted: bool
    engine: str
    dialect: str | None
    reasons: list[str]
    tables: list[str]
    tableAuthorization: str


@router.post("/validate", response_model=SqlValidateResponse)
def validate(request: SqlValidateRequest) -> SqlValidateResponse:
    allowed = None if request.allowed_tables is None else {table.lower() for table in request.allowed_tables}
    result = validate_sql(request.sql, request.engine, allowed)
    return SqlValidateResponse(
        accepted=result.accepted,
        engine=result.engine,
        dialect=result.dialect,
        reasons=result.reasons,
        tables=result.tables,
        tableAuthorization=result.table_authorization,
    )
