FROM python:3
WORKDIR /usr/src/app
COPY cpspreload.py .
COPY samplepreload.json .
RUN pip install requests
CMD ["cpspreload.py"]
ENTRYPOINT ["python3"]
