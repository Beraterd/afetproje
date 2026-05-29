package com.afet.koordinasyon.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatusMessageTemplateKey {

    SAFE_AND_WELL(
        "Şu anda güvendeyim, sağlık durumum iyi."),
    SAFE_BUT_UNCERTAIN(
        "Şu anda güvendeyim ancak bulunduğum bölgede durum belirsiz."),
    OUTSIDE_AND_SAFE(
        "Evden çıktım, dışarıdayım ve şu an iyiyim."),
    HOME_DAMAGED_BUT_SAFE(
        "Evimde hasar var ama ben güvendeyim."),
    NEED_HELP(
        "Bulunduğum yerde yardıma ihtiyaç var."),
    TRAPPED(
        "Göçük altında veya mahsur kalmış olabilirim, acil yardım gerekiyor."),
    CANNOT_SPEAK(
        "Şu an konuşamıyorum, lütfen konum ve resmi kanallar üzerinden durumumu takip edin.");

    private final String messageText;
}
