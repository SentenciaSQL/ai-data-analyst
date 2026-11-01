from collections.abc import Iterator
from typing import Protocol


class ProviderError(RuntimeError):
    """The configured model provider failed or is not available."""


class ChatProvider(Protocol):
    name: str
    configured: bool

    def complete(self, system: str, user: str) -> str:
        """Return the full assistant message."""

    def stream(self, system: str, user: str) -> Iterator[str]:
        """Yield assistant text as the provider produces it."""
