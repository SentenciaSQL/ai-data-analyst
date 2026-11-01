DATA_MARKERS = (
    "revenue",
    "sales",
    "selling",
    "customer",
    "product",
    "profit",
    "invoice",
    "order",
    "trend",
    "margin",
    "kpi",
    "metric",
    "how many",
    "how much",
    "top ",
    "total ",
)


def classify_intent(message: str) -> str:
    text = message.lower()
    if any(marker in text for marker in DATA_MARKERS):
        return "data_question"
    return "product_help"


def system_prompt(intent: str) -> str:
    return f"""You are the AI analyst inside a multi-tenant analytics product.
No customer database is connected to this request. You have no business data.
Do not invent metrics, customers, products, dates, or query results.
Do not emit SQL that you claim was executed.
Do not emit JavaScript or HTML.
Write a short markdown answer.
The question was classified as: {intent}.
If it is a data question, say that a read-only database connection and an approved semantic catalog are required before company figures can be answered.
If it is not a data question, explain that you analyze the company's data once those are in place."""


def static_answer(intent: str) -> str:
    if intent == "data_question":
        return (
            "I can't answer that from your company data yet. This workspace has no connected database "
            "and no approved semantic catalog, so I won't estimate figures.\n\n"
            "After an administrator connects a read-only database and approves the business definitions, "
            "I can turn a question like this into a checked read-only query, explain the result, and recommend a chart."
        )
    return (
        "I'm the analyst for this workspace. I answer questions about your company's data after a database "
        "connection and an approved semantic catalog are in place. I don't invent numbers.\n\n"
        "Team and Settings are available now. Connections, the semantic catalog, dashboards, and query history "
        "are not part of this release."
    )
