ENGINE_DIALECTS = {
    "postgresql": "postgres",
    "mysql": "mysql",
    "sqlserver": "tsql",
    "oracle": "oracle",
    "snowflake": "snowflake",
    "bigquery": "bigquery",
}


def sqlglot_dialect(engine: str) -> str | None:
    return ENGINE_DIALECTS.get(engine.strip().lower())
