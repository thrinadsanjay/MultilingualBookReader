FROM python:3.12-slim
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr tesseract-ocr-eng tesseract-ocr-hin tesseract-ocr-tel \
    espeak-ng ffmpeg \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app ./app
ENV PYTHONUNBUFFERED=1
# Fail the build rather than ship an image that cannot read Telugu.
RUN python -c "import pytesseract, sys; langs = set(pytesseract.get_languages(config='')); missing = {'eng','hin','tel'} - langs; sys.exit(f'missing tesseract languages: {sorted(missing)}') if missing else print('tesseract languages:', sorted(langs))"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD python -c "import urllib.request;urllib.request.urlopen('http://127.0.0.1:8080/health')"
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
