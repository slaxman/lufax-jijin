package com.lufax.jijin.ylx.job.redeem;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.lufax.jijin.base.utils.Logger;
import com.lufax.jijin.scheduler.basejob.BaseJob;
import com.lufax.jijin.ylx.batch.domain.YLXBatchStatus2;
import com.lufax.jijin.ylx.batch.domain.YLXBatchType;
import com.lufax.jijin.ylx.batch.dto.BatchRunStatus;
import com.lufax.jijin.ylx.batch.dto.YLXBatchDTO;
import com.lufax.jijin.ylx.batch.repository.YLXBatchRepository;
import com.lufax.jijin.ylx.confirm.service.BaseConfirmService;
import com.lufax.jijin.ylx.confirm.service.YLXPurchaseConfirmService;
import com.lufax.jijin.ylx.confirm.service.YLXRedeemConfirmService;

@Component
public class YLXHandleRedeemConfirmDataJob extends BaseJob<YLXBatchDTO,Long> {
    @Autowired
    private BaseConfirmService baseConfirmServie;
    @Autowired
    private YLXRedeemConfirmService ylxRedeemConfirmService;
    @Autowired
    private YLXBatchRepository ylxBatchRepository;
    @Override
    protected List<YLXBatchDTO> fetchList(int batchAmount) {
        return baseConfirmServie.getAfterTriggerDateBatchRecords(YLXBatchType.YLX_SLP_REDEEM_CONFIRM, asList(YLXBatchStatus2.RECEIVED), BatchRunStatus.IDLE);
    }

    @Override
    protected int getBatchAmount() {
        return 0;
    }

    @Override
    protected void processList(List<YLXBatchDTO> list) {
        for(YLXBatchDTO batchDTO : list){
            try {
                ylxBatchRepository.updateBatchRunStatusById(batchDTO.getId(),BatchRunStatus.ONGOING);
                ylxRedeemConfirmService.confirmAndUpdateFundBalance(batchDTO);
            }catch (Exception e){
                baseConfirmServie.sendHandleConfirmFailSMSIfTimeout(batchDTO, "15:00","15:30");
                Logger.error(this, String.format("YLXHandleRedeemConfirmDataJob failed,batchId:%s", batchDTO.getId()),e);
            }finally {
                ylxBatchRepository.updateBatchRunStatusById(batchDTO.getId(),BatchRunStatus.IDLE);
            }
        }
    }
}
