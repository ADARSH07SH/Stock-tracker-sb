from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
from fastapi import Response
import time

# Metrics definitions
REQUEST_COUNT = Counter(
    'ai_requests_total',
    'Total number of AI requests',
    ['method', 'endpoint', 'status']
)

REQUEST_DURATION = Histogram(
    'ai_request_duration_seconds',
    'Time spent processing AI requests',
    ['method', 'endpoint']
)

ACTIVE_REQUESTS = Gauge(
    'ai_active_requests',
    'Number of active AI requests'
)

MODEL_USAGE = Counter(
    'ai_model_usage_total',
    'Total usage count per AI model',
    ['model_name', 'status']
)

CHAT_MESSAGES = Counter(
    'ai_chat_messages_total',
    'Total number of chat messages processed',
    ['intent', 'status']
)

IMAGE_GENERATION = Counter(
    'ai_image_generation_total',
    'Total number of image generation requests',
    ['status']
)

RESEARCH_OPERATIONS = Counter(
    'ai_research_operations_total',
    'Total number of stock research operations',
    ['status']
)

class MetricsMiddleware:
    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        method = scope["method"]
        path = scope["path"]
        
        # Skip metrics endpoint
        if path == "/metrics":
            await self.app(scope, receive, send)
            return

        start_time = time.time()
        ACTIVE_REQUESTS.inc()

        async def send_wrapper(message):
            if message["type"] == "http.response.start":
                status_code = message["status"]
                duration = time.time() - start_time
                
                REQUEST_COUNT.labels(
                    method=method,
                    endpoint=path,
                    status=status_code
                ).inc()
                
                REQUEST_DURATION.labels(
                    method=method,
                    endpoint=path
                ).observe(duration)
                
                ACTIVE_REQUESTS.dec()
            
            await send(message)

        await self.app(scope, receive, send_wrapper)

def record_model_usage(model_name: str, status: str = "success"):
    """Record usage of AI model"""
    MODEL_USAGE.labels(model_name=model_name, status=status).inc()

def record_chat_message(intent: str, status: str = "success"):
    """Record chat message processing"""
    CHAT_MESSAGES.labels(intent=intent, status=status).inc()

def record_image_generation(status: str = "success"):
    """Record image generation request"""
    IMAGE_GENERATION.labels(status=status).inc()

def record_research_operation(status: str = "success"):
    """Record stock research operation"""
    RESEARCH_OPERATIONS.labels(status=status).inc()

def get_metrics():
    """Get Prometheus metrics"""
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)