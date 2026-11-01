from collections.abc import Iterator

from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage, SystemMessage

from app.providers.base import ProviderError
from app.providers.text import message_text


class AnthropicProvider:
    def __init__(self, api_key: str, model: str) -> None:
        self.name = "anthropic"
        self._api_key = api_key.strip()
        self._model_name = model
        self.configured = bool(self._api_key)

    def complete(self, system: str, user: str) -> str:
        return "".join(self.stream(system, user))

    def stream(self, system: str, user: str) -> Iterator[str]:
        if not self.configured:
            raise ProviderError("Anthropic is not configured.")
        model = ChatAnthropic(model=self._model_name, api_key=self._api_key, temperature=0.2, timeout=60)
        try:
            for chunk in model.stream([SystemMessage(content=system), HumanMessage(content=user)]):
                text = message_text(chunk.content)
                if text:
                    yield text
        except Exception as exc:
            raise ProviderError("The Anthropic request failed.") from exc
