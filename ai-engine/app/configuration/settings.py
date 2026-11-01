from functools import lru_cache

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(extra="ignore")

    environment: str = Field(default="local", validation_alias=AliasChoices("ADA_ENV", "ENVIRONMENT"))
    internal_api_key: str = Field(default="dev-only-internal-key", validation_alias="INTERNAL_API_KEY")
    ai_provider: str = Field(default="openai", validation_alias="AI_PROVIDER")
    openai_api_key: str = Field(default="", validation_alias="OPENAI_API_KEY")
    openai_model: str = Field(default="gpt-4o-mini", validation_alias="OPENAI_MODEL")
    anthropic_api_key: str = Field(default="", validation_alias="ANTHROPIC_API_KEY")
    anthropic_model: str = Field(default="claude-sonnet-4-5", validation_alias="ANTHROPIC_MODEL")
    log_level: str = Field(default="INFO", validation_alias="LOG_LEVEL")

    def assert_production_safe(self) -> None:
        if self.environment.lower() != "production":
            return
        if "dev-only" in self.internal_api_key:
            raise RuntimeError("Refusing the development internal API key while ADA_ENV=production.")


@lru_cache
def get_settings() -> Settings:
    return Settings()
