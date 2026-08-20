"""FastAPI application entrypoint for the AI Story Co-Author AI Service."""
from __future__ import annotations

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.router import router
from app.config import settings

app = FastAPI(title="AI Story Co-Author — AI Service", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(router, prefix="/ai")


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "version": "0.1.0",
        "mock_llm": settings.using_mock_llm,
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host=settings.ai_service_host,
        port=settings.ai_service_port,
        reload=False,
    )
