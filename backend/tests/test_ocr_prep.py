from app.services.ocr_prep import (
    MIN_OCR_WIDTH,
    missing_languages,
    normalise_hint,
    page_segmentation_modes,
    tesseract_language,
    upscale_factor,
)


def test_hint_is_reduced_to_a_bare_subtag():
    assert normalise_hint("te-IN") == "te"
    assert normalise_hint("TE") == "te"
    assert normalise_hint("hi_IN") == "hi"
    assert normalise_hint(None) == ""


def test_telugu_loads_english_alongside_it():
    assert tesseract_language("te") == "tel+eng"
    assert tesseract_language("te-IN") == "tel+eng"


def test_other_languages_keep_their_own_chains():
    assert tesseract_language("hi") == "hin+eng"
    assert tesseract_language("en") == "eng"
    assert tesseract_language(None) == "eng+hin+tel"


def test_uninstalled_models_are_dropped_rather_than_crashing_tesseract():
    assert tesseract_language("te", available={"eng"}) == "eng"
    assert tesseract_language("te", available={"tel", "eng"}) == "tel+eng"
    assert tesseract_language(None, available={"eng", "tel"}) == "eng+tel"


def test_missing_languages_are_reported_for_the_error_message():
    assert missing_languages("te", available={"eng"}) == ["tel"]
    assert missing_languages("te", available={"tel", "eng"}) == []


def test_small_pages_are_upscaled_and_large_ones_left_alone():
    assert upscale_factor(MIN_OCR_WIDTH) == 1.0
    assert upscale_factor(3000) == 1.0
    assert upscale_factor(700) == 2.0


def test_upscaling_is_capped_so_a_tiny_crop_cannot_explode():
    factor = upscale_factor(100, min_width=1400, max_width=3500)
    assert 100 * factor <= 3500


def test_zero_width_is_handled():
    assert upscale_factor(0) == 1.0


def test_segmentation_modes_start_with_a_single_text_block():
    assert page_segmentation_modes("te")[0] == 6
    assert len(page_segmentation_modes("te")) == 3
