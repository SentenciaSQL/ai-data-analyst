from typing import TypedDict

from langgraph.graph import END, START, StateGraph

from app.providers.base import ChatProvider, ProviderError
from app.workflows.prompts import classify_intent, static_answer, system_prompt


class AnalystState(TypedDict, total=False):
    message: str
    intent: str
    answer: str


def build_graph(provider: ChatProvider):
    def classify(state: AnalystState) -> dict[str, str]:
        return {"intent": classify_intent(state["message"])}

    def compose(state: AnalystState) -> dict[str, str]:
        intent = state["intent"]
        if provider.configured:
            try:
                answer = provider.complete(system_prompt(intent), state["message"])
            except ProviderError:
                answer = static_answer(intent)
        else:
            answer = static_answer(intent)
        return {"answer": answer.strip()}

    graph = StateGraph(AnalystState)
    graph.add_node("classify_intent", classify)
    graph.add_node("compose_answer", compose)
    graph.add_edge(START, "classify_intent")
    graph.add_edge("classify_intent", "compose_answer")
    graph.add_edge("compose_answer", END)
    return graph.compile()
