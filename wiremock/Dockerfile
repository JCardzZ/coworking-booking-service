# payment provider mock for the Render demo, same mappings as docker compose
FROM wiremock/wiremock:3.13.2
COPY mappings /home/wiremock/mappings
CMD ["--port", "8080", "--disable-banner"]
