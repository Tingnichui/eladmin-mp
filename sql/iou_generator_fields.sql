-- 同步两张表字段后执行，配置电子借条生成器字段，不改变业务表。
USE `eladmin`;
SET NAMES utf8mb4;
UPDATE code_column SET list_show=b'0',form_show=b'0',not_null=b'0',query_type=NULL,dict_name=NULL WHERE data_source='master' AND table_name='iou_user' AND column_name IN ('id','password_hash','id_no_encrypted','id_no_hash');
UPDATE code_column SET query_type='=',remark='手机号' WHERE data_source='master' AND table_name='iou_user' AND column_name='mobile';
UPDATE code_column SET query_type='Like' WHERE data_source='master' AND table_name='iou_user' AND column_name='real_name';
UPDATE code_column SET form_type='Select',dict_name='iou_user_verify_status',query_type='=',remark='认证状态',not_null=b'0' WHERE data_source='master' AND table_name='iou_user' AND column_name='verify_status';
UPDATE code_column SET form_show=b'0',not_null=b'0' WHERE data_source='master' AND table_name='iou_user' AND column_name IN ('verified_at','create_time','update_time');
UPDATE code_column SET query_type='BetWeen' WHERE data_source='master' AND table_name='iou_user' AND column_name='create_time';
UPDATE code_column SET list_show=b'0',form_show=b'0',not_null=b'0',query_type=NULL,dict_name=NULL WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('id','borrower_id_no_hash','borrower_id_no_encrypted','lender_id_no_hash','lender_id_no_encrypted');
UPDATE code_column SET form_show=b'0',not_null=b'0' WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('borrower_signed_at','lender_signed_at','effective_at','create_time','update_time');
UPDATE code_column SET list_show=b'0' WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('purpose','remark','agreement_file','cancel_reason','update_time');
UPDATE code_column SET query_type='=' WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('note_no','creator_user_id');
UPDATE code_column SET form_type='Input',remark='发起人ID' WHERE data_source='master' AND table_name='iou_note' AND column_name='creator_user_id';
-- 用户端签署字段由专用服务维护；同步生成配置时不暴露内部快照和凭证。
UPDATE code_column SET list_show=b'0',form_show=b'0',not_null=b'0',query_type=NULL,dict_name=NULL WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('create_request_key','create_request_hash','active_sign_context');
UPDATE code_column SET list_show=b'0',form_show=b'0',not_null=b'0',query_type=NULL,dict_name=NULL WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('terms_snapshot','terms_hash','terms_locked_at','borrower_sign_status','borrower_sign_request_id','borrower_sign_evidence_id','borrower_sign_file','borrower_sign_file_hash','lender_sign_status','lender_sign_request_id','lender_sign_evidence_id','lender_sign_file','lender_sign_file_hash','agreement_hash','row_version');
UPDATE code_column SET query_type='Like' WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('borrower_name','lender_name');
UPDATE code_column SET form_type='Select',dict_name='iou_note_loan_type',query_type='=',remark='借款类型' WHERE data_source='master' AND table_name='iou_note' AND column_name='loan_type';
UPDATE code_column SET form_type='Select',dict_name='iou_note_status',query_type='=',remark='借条状态' WHERE data_source='master' AND table_name='iou_note' AND column_name='status';
UPDATE code_column SET query_type='BetWeen' WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('due_date','create_time');
UPDATE code_column SET form_type='Textarea' WHERE data_source='master' AND table_name='iou_note' AND column_name IN ('purpose','remark','cancel_reason');
UPDATE code_column SET remark='借款人' WHERE data_source='master' AND table_name='iou_note' AND column_name='borrower_name';
UPDATE code_column SET remark='出借人' WHERE data_source='master' AND table_name='iou_note' AND column_name='lender_name';
UPDATE code_column SET remark='本金' WHERE data_source='master' AND table_name='iou_note' AND column_name='principal_amount';
UPDATE code_column SET remark='年利率（0.15表示15%）' WHERE data_source='master' AND table_name='iou_note' AND column_name='annual_rate';
UPDATE code_column SET remark='服务费' WHERE data_source='master' AND table_name='iou_note' AND column_name='service_fee';
