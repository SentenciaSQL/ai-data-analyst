from app.configuration.settings import Settings
from app.providers.anthropic_provider import AnthropicProvider
from app.providers.base import ChatProvider
from app.providers.openai_provider import OpenAiProvider


class UnavailableProvider:
    name = "unconfigured"
    configured = False

    def complete(self, system: str, user: str) -> str:
        raise RuntimeError("No AI provider is configured.")

    def stream(self, system: str, user: str):
        raise RuntimeError("No AI provider is configured.")
        yield ""


def build_provider(settings: Settings) -> ChatProvider:
    provider = settings.ai_provider.strip().lower()
    if provider == "anthropic":
        return AnthropicProvider(settings.anthropic_api_key, settings.anthropic_model)
    if provider == "openai":
        return OpenAiProvider(settings.openai_api_key, settings.openai_model)
    return UnavailableProvider()
