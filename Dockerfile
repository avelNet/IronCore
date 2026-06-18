# ─────────────────────────────────────────────────────────────────────────────
# IronCore — Dockerfile для воспроизводимой сборки мода
#
# Использование:
#   docker compose up --build       # собрать мод внутри контейнера
#   docker compose run builder bash  # интерактивный режим
#
# Артефакт появится в ./build/libs/ironcore-*.jar на хост-машине
# благодаря bind-mount из docker-compose.yml.
# ─────────────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="avelNet <avel.strelnikov@outlook.com>"
LABEL description="IronCore Minecraft Forge mod — reproducible build environment"

# Git нужен ForgeGradle для определения источника маппингов
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Сначала копируем только Gradle-файлы — Docker кэширует этот слой
# и не перекачивает зависимости при изменениях только в src/
COPY gradle/           gradle/
COPY gradlew           gradlew.bat  ./
COPY build.gradle      settings.gradle  gradle.properties  ./

RUN chmod +x gradlew \
    # Прогреваем кэш зависимостей (Forge + GeckoLib, ~600 MB при первом запуске)
    && ./gradlew dependencies --no-daemon --quiet || true

# Копируем исходный код после прогрева кэша зависимостей
COPY src/ src/

# По умолчанию — полная сборка
CMD ["./gradlew", "build", "--no-daemon"]
