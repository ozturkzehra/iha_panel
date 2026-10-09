# İHA Panel – Uçuş Hava Durumu Brifingi

Android (Kotlin / Jetpack Compose) uygulaması. Seçilen İHA platformu için kalkış, görev sahası ve
iniş/alternatif noktalarının meteorolojik durumunu **şimdi + önümüzdeki 3 saat** için değerlendirir
ve GO / CAUTION / NO-GO kararı üretir.

## Mimari
- `domain/` – saf Kotlin, Android bağımsız, birim testli: `FlightEvaluator` (kurallar), `MissionPlanner`
  (nokta × zaman toplulaştırma), `AviationMath` (yan rüzgâr, yoğunluk irtifası, çiğ noktası…), `UavPlatform` (limitler).
- `data/` – Open-Meteo (Retrofit/Gson), `WeatherMapper` (eksik veriyi açıkça reddeder).
- `ui/` – `WeatherViewModel` (ham veriyi saklar; platform/pist değişince ağsız yeniden değerlendirir), Compose ekranı.

## Değerlendirilen faktörler
| Faktör | NO-GO | CAUTION |
|---|---|---|
| Yüzey rüzgârı | > limit | limit − 10 km/h … limit |
| Hamle | > limit | limit − 10 … limit |
| Yan rüzgâr (pist yönü girilirse; hamle dahil) | > limit | limit − 6 … limit |
| İrtifa rüzgârı (80/120/180 m'nin en büyüğü) | > limit | limit − 15 … limit |
| Görüş | < asgari | < asgari × 1,5 |
| Bulut tabanı (tahmini, yalnız ≥ %62,5 düşük bulutta) | < asgari | < asgari × 1,5 |
| Hadise (WMO kodu) | fırtına, dondurucu yağış, yoğun kar | kar, sis |
| Yağış | > limit | > 0 |
| Sıcaklık / buzlanma | < −20 / > 45 °C; 0…−20 °C + görünür nem | +5…−20 °C + nem; kuru donma altı |
| Yoğunluk irtifası | > limit | limit − 500 m … limit |
| Veri yaşı | > 90 dk | > 45 dk |
| Risk birikimi | ≥ 3 CAUTION birlikte → NO-GO | |

Tüm bulgular listelenir (ilk eşleşen kazanmaz); genel karar en kötü bulgudur. Herhangi bir noktanın verisi
alınamazsa karar **verilemez** (GO sayılmaz).

Notlar:
- Zorunlu alanlar (sıcaklık, rüzgâr, hamle, basınç, yağış, hava kodu…) eksikse veri reddedilir. İrtifa rüzgârı
  ve düşük bulut eksikse ilgili kural atlanır ve ekranda not gösterilir. Tahmin saatlerinde eksik alan varsa o saat atlanır.
- Veri yaşı kuralı yalnızca "şimdi" değerlendirmesinde çalışır; bu uyarı risk birikimi sayımına da dahildir.
- Buzlanma uyarısındaki "nem": yağış, hava kodu ≥ 45 veya sıcaklık − çiğ noktası ≤ 3 °C.
- "Son gözlem" Open-Meteo'nun 15 dakikalık model zaman damgasıdır; "Son yenileme" telefonun saatidir.
- Gece operasyonu bilgi notu olarak gösterilir, karar vermez.
- Kalkış noktası GPS'ten alınır; konum yoksa İstanbul kullanılır. Görev sahası ve iniş için hazır şehir listesi
  veya "Kalkışla aynı" seçilebilir.