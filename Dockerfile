# ─────────────────────────────────────────────────────────────────────────────
# IronCore — Dockerfile для воспроизводимой сборки мода
#
# Использование:
#   docker compose up --build       # собрать мод внутри контейнера
#   docker compose run builder bash  # интерактивный режим
#
# Артефакт появится в ./build/libs/ironcore-*.jar на хост-машине
# благодаря bind-mount из docker-compose.yml.
#
# Кэш зависимостей (Forge + GeckoLib, ~600 MB) хранится в именованном volume
# gradle-cache, смонтированном в /root/.gradle (см. docker-compose.yml). Поэтому
# прогрев кэша на этапе build не делается: volume монтируется только в runtime
# и перекрыл бы любой прогретый при сборке образа кэш. Первый `up` скачивает
# зависимости в volume, последующие запуски берут их оттуда.
# ─────────────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="avelNet <avel.strelnikov@outlook.com>"
LABEL description="IronCore Minecraft Forge mod — reproducible build environment"

# Git нужен ForgeGradle для определения источника маппингов
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Сначала Gradle-обвязка и build-скрипты — отдельный слой, не инвалидируется
# при изменениях только в src/
COPY gradle/      gradle/
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew

# Исходный код — отдельным слоем
COPY src/ src/

# Полная сборка; зависимости кэшируются в смонтированном gradle-cache volume
CMD ["./gradlew", "build", "--no-daemon"]
