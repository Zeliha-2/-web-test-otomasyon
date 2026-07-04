# LaTeX Rapor Derleme

Bitirme / vize raporu kaynak dosyaları.

## Dosyalar

| Dosya | Açıklama |
|-------|----------|
| `rapor.tex` | Ana belge (kapak, TOC, bölümler, kaynakça) |
| `icindekiler.tex` | Tahmini sayfa numaralı manuel içindekiler |
| `bolumler/*.tex` | Bölüm içerikleri (TODO işaretli yerleri doldur) |

## Derleme

TeX Live veya MiKTeX gerekir.

```bash
cd docs/latex
pdflatex rapor.tex
pdflatex rapor.tex
```

İkinci derleme içindekiler ve çapraz referanslar için gereklidir.

## Özelleştirme

1. `rapor.tex` içinde `\ogrenciAd`, `\danisman`, `\universite` alanlarını doldur.
2. `bolumler/` altındaki `% TODO:` satırlarını tamamla.
3. Şekil placeholder'larını TikZ veya PNG ekran görüntüleri ile değiştir.
4. `09-sonuclar.tex` tablosuna son test koşumu pass/fail sayılarını yaz.
5. Üniversite şablonu farklıysa `\documentclass` ve kapak sayfasını uyarla.

## Manuel vs otomatik içindekiler

- Otomatik: `\tableofcontents` (rapor.tex'te aktif)
- Manuel tahmini: `icindekiler.tex` (sunum / onay için)

İkisini birlikte kullanmak istemezsen `rapor.tex`'te `\input{icindekiler}` satırını yorum satırı yap.
