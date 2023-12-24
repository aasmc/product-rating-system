# Start from the base OpenSearch image
FROM opensearchproject/opensearch:latest

# Copy Hunspell dictionary files for Russian
COPY hunspell/ru_RU.aff /usr/share/opensearch/config/hunspell/ru_RU/ru_RU.aff
COPY hunspell/ru_RU.dic /usr/share/opensearch/config/hunspell/ru_RU/ru_RU.dic
COPY hunspell/settings.yml /usr/share/opensearch/config/hunspell/ru_RU/settings.yml