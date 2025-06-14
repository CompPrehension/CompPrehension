# syntax=docker/dockerfile:1
FROM python:3.11-slim AS build
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential g++ libopenblas-dev liblapack-dev libomp-dev \
    && rm -rf /var/lib/apt/lists/*

COPY modules/bkt/src/main/python/requirements.txt /tmp/
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r /tmp/requirements.txt

WORKDIR /build
COPY modules/bkt/src/main/python/ ./python
COPY modules/bkt/src/main/proto/ ./proto

RUN python -m grpc_tools.protoc \
        -I ./proto \
        --python_out=./python \
        --grpc_python_out=./python \
        ./proto/bkt-service.proto

FROM python:3.11-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
        libgomp1 libopenblas-dev liblapack-dev libomp-dev && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /usr/local /usr/local

WORKDIR /srv/app
COPY --from=build /build/python/ /srv/app/

ENV PYTHONUNBUFFERED=1 \
    PYTHONPATH=/srv/app

EXPOSE 50051
CMD ["python", "bkt_service.py"]
