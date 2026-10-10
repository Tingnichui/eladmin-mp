package me.zhengjie.iou;

import com.alibaba.fastjson2.JSON;
import me.zhengjie.exception.BadRequestException;
import me.zhengjie.iou.domain.IouNote;
import me.zhengjie.iou.domain.IouUser;
import me.zhengjie.iou.service.IouIdentitySupport;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IouSensitiveDataTest {
    private final IouIdentitySupport identity = new IouIdentitySupport(
        "unit-test-encryption-password", "unit-test-hmac-key-with-at-least-32-characters");

    @Test
    void normalizedIdentityMatchesAcrossUserAndNoteAndCiphertextIsRandomized() {
        String syntheticId = "00000020000101000x";
        assertEquals(identity.hash(syntheticId), identity.hash(" 00000020000101000X "));
        assertNotEquals(identity.hash(syntheticId), identity.hash("000000200001010001"));
        assertEquals(64, identity.hash(syntheticId).length());
        assertNotEquals(identity.encrypt(syntheticId), identity.encrypt(syntheticId));
        assertFalse(identity.encrypt(syntheticId).contains(syntheticId));
    }

    @Test
    void identifiersBeyondJavascriptSafeIntegerAreSerializedAsStrings() {
        long id = 9007199254740993L;
        IouUser user = new IouUser();
        user.setId(id);
        IouNote note = new IouNote();
        note.setId(id);
        note.setCreatorUserId(id);
        assertEquals("9007199254740993", JSON.parseObject(JSON.toJSONString(user)).get("id"));
        assertEquals("9007199254740993", JSON.parseObject(JSON.toJSONString(note)).get("id"));
        assertEquals("9007199254740993", JSON.parseObject(JSON.toJSONString(note)).get("creatorUserId"));
    }

    @Test
    void missingHmacKeyDoesNotSilentlyUseAnUnkeyedHash() {
        IouIdentitySupport unconfigured = new IouIdentitySupport("test-password", "");
        assertThrows(BadRequestException.class, () -> unconfigured.hash("000000200001010001"));
    }

    @Test
    void userSecretsAreAcceptedOnlyThroughPlaintextInputAndNeverSerialized() {
        IouUser user = JSON.parseObject("{\"password\":\"test-password\",\"idNo\":\"000000200001010001\"}", IouUser.class);
        assertEquals("test-password", user.getPassword());
        user.setIdNoHash("sensitive-id-hash");
        user.setIdNoEncrypted("sensitive-id-ciphertext");
        String json = JSON.toJSONString(user);
        assertFalse(json.contains("test-password"));
        assertFalse(json.contains("000000200001010001"));
        assertFalse(json.contains("sensitive-"));
        assertFalse(user.toString().contains("sensitive-"));
    }

    @Test
    void noteSecretsAreNotReturnedOrWrittenIntoAuditParameters() {
        IouNote note = new IouNote();
        note.setBorrowerIdNo("000000200001010001");
        note.setLenderIdNo("000000200001010002");
        note.setBorrowerIdNoHash("sensitive-borrower-hash");
        note.setLenderIdNoHash("sensitive-lender-hash");
        note.setBorrowerIdNoEncrypted("sensitive-borrower-ciphertext");
        note.setLenderIdNoEncrypted("sensitive-lender-ciphertext");
        String json = JSON.toJSONString(note);
        assertFalse(json.contains("000000"));
        assertFalse(json.contains("sensitive-"));
        assertFalse(note.toString().contains("000000"));
        assertFalse(note.toString().contains("sensitive-"));
    }
}
