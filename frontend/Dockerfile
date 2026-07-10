FROM node:22-alpine

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

EXPOSE 4200

ENV NG_ALLOWED_HOSTS=localhost,127.0.0.1

CMD ["npm", "start", "--", "--host", "0.0.0.0"]
