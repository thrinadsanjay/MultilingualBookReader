from app.services.language import detect_language, detect_spans


def test_english():
    assert detect_language("Today we will read a good story.") == "en"


def test_hindi():
    assert detect_language("आज हम एक अच्छी कहानी पढ़ेंगे।") == "hi"


def test_telugu():
    assert detect_language("ఈ రోజు మనం ఒక మంచి కథ చదువుదాం.") == "te"


def test_mixed():
    assert detect_language("రాముడు went to the market.") == "mul"
    langs = {lang for lang, _ in detect_spans("రాముడు went to the market.")}
    assert "te" in langs
    assert "en" in langs
