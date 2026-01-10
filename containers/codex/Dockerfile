FROM ubuntu:24.04

ARG NODE_VERSION=20
ENV NODE_VERSION=${NODE_VERSION}

RUN apt-get update && apt-get install -y --no-install-recommends \
  git \
  curl \
  zsh \
  ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# install nvm
ENV NVM_DIR=/root/.nvm
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash

# default to zsh for the root user
COPY root/.zshrc /root/.zshrc
RUN sed -i -e "s#/bin/bash#/bin/zsh#" /etc/passwd

COPY scripts/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["zsh"]
