package me.zhengjie.iou.mapper;

import org.apache.ibatis.annotations.*;

/** The daily counter is locked until the surrounding draft transaction commits. */
@Mapper
public interface IouNoteNumberMapper {
    @Insert("INSERT INTO iou_note_number (number_date, `last_value`) VALUES (#{day}, 0) "
        + "ON DUPLICATE KEY UPDATE number_date = VALUES(number_date)")
    void ensureDay(@Param("day") String day);

    @Select("SELECT `last_value` FROM iou_note_number WHERE number_date = #{day} FOR UPDATE")
    Long lockDay(@Param("day") String day);

    @Update("UPDATE iou_note_number SET `last_value` = #{value} WHERE number_date = #{day}")
    int saveValue(@Param("day") String day, @Param("value") long value);
}
