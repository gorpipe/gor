FROM adoptopenjdk:11-jre-hotspot

ARG GOR_INSTALL_DIR=/opt/nextcode/gor-scripts

RUN mkdir -p /opt/nextcode
COPY gorscripts/build/install/gorscripts ${GOR_INSTALL_DIR}

RUN useradd --create-home --shell /bin/bash gor
USER gor
WORKDIR /home/gor

ENV PATH=${GOR_INSTALL_DIR}/bin:$PATH

CMD bash
