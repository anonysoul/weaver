FROM ubuntu:24.04

ARG NODE_VERSION=
ENV NODE_VERSION=${NODE_VERSION}

ARG CODE_SERVER_VERSION=4.107.1
ENV CODE_SERVER_VERSION=${CODE_SERVER_VERSION}

RUN apt-get update && apt-get install -y --no-install-recommends \
  git \
  git-lfs \
  curl \
  wget \
  zsh \
  vim \
  ca-certificates \
  && rm -rf /var/lib/apt/lists/*

RUN curl -fSL "https://github.com/coder/code-server/releases/download/v${CODE_SERVER_VERSION}/code-server_${CODE_SERVER_VERSION}_amd64.deb" \
  -o /tmp/code-server.deb \
  && apt-get update \
  && apt-get install -y --no-install-recommends /tmp/code-server.deb \
  && rm -f /tmp/code-server.deb \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /root/.local/share/code-server/User \
  && printf '%s\n' '{"workbench.colorTheme":"Default Dark Modern"}' \
  > /root/.local/share/code-server/User/settings.json

# default to zsh for the root user
RUN sed -i -e "s#/bin/bash#/bin/zsh#" /etc/passwd

# install ohmyzsh
RUN sh -c "$(wget -O- https://github.com/deluan/zsh-in-docker/releases/download/v1.2.1/zsh-in-docker.sh)" -- \
    -t bira

# install nvm
ENV NVM_DIR=/root/.nvm
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.3/install.sh | bash
RUN printf '\nexport NVM_DIR="$HOME/.nvm"\n[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"\n[ -s "$NVM_DIR/bash_completion" ] && . "$NVM_DIR/bash_completion"\n' >> /root/.zshrc

RUN mkdir -p /root/workspace
WORKDIR /root/workspace

EXPOSE 8080

COPY scripts/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["zsh"]
