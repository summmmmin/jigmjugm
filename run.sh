#!/bin/bash

# Load environment variables from .env file
export DATABASE_URL=jdbc:postgresql://localhost:6000/jigmjugm
export DATABASE_USERNAME=jj_user
export DATABASE_PASSWORD=jj_pass
export KAKAO_CLIENT_ID=5918d42a00bed663612727e9a49c7f64
export KAKAO_CLIENT_SECRET=CuegOFgl5VRggsANPsqKv0a2I0pbG9Dj
export KAKAO_REDIRECT_URI=http://localhost:8080/api/v1/login/oauth2/code/kakao
export SERVER_PORT=8080

# Run Spring Boot application
./gradlew bootRun