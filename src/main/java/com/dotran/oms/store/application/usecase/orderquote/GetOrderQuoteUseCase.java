package com.dotran.oms.store.application.usecase.orderquote;

import com.dotran.oms.store.application.command.orderquote.GetOrderQuoteCmd;
import com.dotran.oms.store.application.dto.orderquote.OrderQuoteDto;

public interface GetOrderQuoteUseCase {

    OrderQuoteDto get(GetOrderQuoteCmd cmd);
}
