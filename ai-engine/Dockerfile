FROM python:3.12.16-slim-bookworm AS runtime
WORKDIR /app
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
COPY app ./app
RUN useradd --system --uid 10001 --create-home app && chown -R app:app /app
USER app
EXPOSE 8090
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=10 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8090/health')"]
ENTRYPOINT ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
