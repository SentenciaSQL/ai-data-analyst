from dataclasses import dataclass, field

import sqlglot
from sqlglot import exp
from sqlglot.errors import ParseError

from app.dialects.registry import sqlglot_dialect

READ_TYPES = (exp.Select, exp.Union, exp.Intersect, exp.Except)
BLOCKED_FUNCTIONS = {
    "pg_sleep",
    "pg_read_file",
    "pg_read_binary_file",
    "pg_ls_dir",
    "pg_stat_file",
    "lo_import",
    "lo_export",
    "dblink",
    "dblink_exec",
    "pg_terminate_backend",
    "pg_cancel_backend",
}
FORBIDDEN_NAMES = (
    "Insert",
    "Update",
    "Delete",
    "Drop",
    "Alter",
    "Create",
    "Command",
    "Merge",
    "Grant",
    "Revoke",
    "Set",
    "Transaction",
    "Copy",
    "TruncateTable",
    "Describe",
    "Use",
    "Kill",
    "DropPartition",
)


@dataclass
class ValidationResult:
    accepted: bool
    engine: str
    dialect: str | None
    reasons: list[str] = field(default_factory=list)
    tables: list[str] = field(default_factory=list)
    table_authorization: str = "deferred"


def validate_sql(sql: str, engine: str, allowed_tables: set[str] | None = None) -> ValidationResult:
    dialect = sqlglot_dialect(engine)
    if dialect is None:
        return ValidationResult(False, engine, None, [f"Unsupported database engine '{engine}'."])
    text = sql.strip()
    if not text:
        return ValidationResult(False, engine, dialect, ["SQL is empty."])
    try:
        statements = [statement for statement in sqlglot.parse(text, read=dialect) if statement is not None]
    except ParseError:
        return ValidationResult(False, engine, dialect, ["SQL could not be parsed for this engine."])
    if len(statements) != 1:
        return ValidationResult(False, engine, dialect, ["Only one SQL statement is allowed."])
    statement = statements[0]
    reasons: list[str] = []
    if not isinstance(statement, READ_TYPES):
        reasons.append("Only a read-only query is allowed.")
    forbidden = tuple(getattr(exp, name) for name in FORBIDDEN_NAMES if hasattr(exp, name))
    if forbidden and statement.find(*forbidden) is not None:
        reasons.append("The statement contains a command that changes data or database structure.")
    if isinstance(statement, exp.Select) and statement.args.get("into") is not None:
        reasons.append("SELECT INTO is not allowed.")
    if isinstance(statement, exp.Select) and statement.args.get("locks"):
        reasons.append("Row-locking clauses are not allowed.")
    for function in statement.find_all(exp.Anonymous, exp.Func):
        name = _function_name(function)
        if name in BLOCKED_FUNCTIONS:
            reasons.append(f"Function {name} is not allowed.")
            break
    tables = _tables(statement)
    authorization = "deferred"
    if allowed_tables is not None:
        authorization = "enforced"
        unexpected = sorted(table for table in tables if table not in allowed_tables)
        if unexpected:
            reasons.append("The query references tables outside the approved schema.")
    unique_reasons = list(dict.fromkeys(reasons))
    return ValidationResult(
        accepted=not unique_reasons,
        engine=engine.strip().lower(),
        dialect=dialect,
        reasons=unique_reasons,
        tables=tables,
        table_authorization=authorization,
    )


def _function_name(function: exp.Expression) -> str:
    name = getattr(function, "name", None)
    if isinstance(name, str) and name:
        return name.lower()
    sql_name = getattr(function, "sql_name", None)
    if callable(sql_name):
        return str(sql_name()).lower()
    return function.key.lower()


def _tables(statement: exp.Expression) -> list[str]:
    cte_names = {cte.alias_or_name.lower() for cte in statement.find_all(exp.CTE)}
    found: list[str] = []
    for table in statement.find_all(exp.Table):
        name = table.name.lower()
        if not name or name in cte_names and not table.db:
            continue
        schema = str(table.db).lower() if table.db else ""
        qualified = f"{schema}.{name}" if schema else name
        if qualified not in found:
            found.append(qualified)
    return found
