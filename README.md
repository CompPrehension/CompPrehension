## CompPrehension

https://taiga.seschool.ru/project/oasychev-modelirovanie-ponimaniia-iadro/wiki/home

### Building and Running

To build and run this project you must have the following items installed:

+ [Java 21](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
+ A tool for checking out a [Git](http://git-scm.com/) repository
+ Apache's [Maven](http://maven.apache.org/index.html)
+ [MySQL server](https://dev.mysql.com/downloads/mysql/)
+ [Node.js](https://nodejs.org/) 22+ to build the frontend

Build it with Maven:
    mvn clean install

You can then run the application as follows:

    mvn spring-boot:run

### Frontend

The React frontend lives in `modules/server` and is built with Vite:

    cd modules/server
    npm install
    npm run build     # production bundle
    npm run dev       # dev server with HMR on https://localhost:4200
    npm run dev:mock  # dev server with mocked backend

![Java CI with Maven](https://github.com/procudin/OntoQuiz/workflows/Java%20CI%20with%20Maven/badge.svg)
