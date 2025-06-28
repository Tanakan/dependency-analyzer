FROM mcr.microsoft.com/devcontainers/java:1-17-bullseye

# Install Node.js 18
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs

# Install additional tools
RUN apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    vim \
    tree \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Set up Maven cache directory
RUN mkdir -p /home/vscode/.m2/repository \
    && chown -R vscode:vscode /home/vscode/.m2

# Install global npm packages
RUN npm install -g @types/node typescript

USER vscode