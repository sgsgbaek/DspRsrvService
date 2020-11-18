package com.danspot.dsprsrvservice.VO;

import lombok.Data;

public @Data
class MmsMessage {
    String fromAddress;
    String mmsText;
}
