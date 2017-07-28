[![Join the chat at https://gitter.im/metasfresh/metasfresh](https://badges.gitter.im/metasfresh/metasfresh.svg)](https://gitter.im/metasfresh/metasfresh?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Krihelimeter](http://krihelinator.xyz/badge/metasfresh/metasfresh-webui-api)](http://krihelinator.xyz)

#metasfresh-webui-api

This repo contains the API service of our webui. The frontend can be found in its [own repository](https://github.com/metasfresh/metasfresh-webui-frontend).

#Some notes for developers:

* one can run it from eclipse
* the `main` method is located at `/metasfresh-webui-api/src/main/java/de/metas/ui/web/WebRestApiApplication.java`
  * if running it for the first time, you probably need to run with `-Dwebui-api-run-headless=false` so that the swing dialog to set the DB connection can be displayed
  * if you also run metasfresh-admin locally on port 9090, you might also want to run the this API with `-Dspring.boot.admin.url=http://localhost:9090 -Dmanagement.security.enabled=false`
* by default, it listens on port 8080
* swagger UI url: http://localhost:8080/swagger-ui.html

