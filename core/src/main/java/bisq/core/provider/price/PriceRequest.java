/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.provider.price;

import bisq.common.util.Utilities;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class PriceRequest {
    private static final ListeningExecutorService executorService = Utilities.getListeningExecutorService("PriceRequest", 3, 5, 10 * 60);
    @Nullable
    private PriceProvider provider;
    private boolean shutDownRequested;

    public PriceRequest() {
    }

    public SettableFuture<PricenodeDto> requestAllPrices(PriceProvider provider) {
        this.provider = provider;
        String baseUrl = provider.getBaseUrl();
        SettableFuture<PricenodeDto> resultFuture = SettableFuture.create();
        ListenableFuture<PricenodeDto> future = executorService.submit(provider::getAll);

        Futures.addCallback(future, new FutureCallback<>() {
            public void onSuccess(PricenodeDto pricenodeDto) {
                if (!shutDownRequested) {
                    resultFuture.set(pricenodeDto);
                }
            }

            public void onFailure(@NotNull Throwable throwable) {
                if (!shutDownRequested && !resultFuture.setException(new PriceRequestException(throwable, baseUrl))) {
                    // In case the setException returns false we need to cancel the future.
                    resultFuture.cancel(true);
                }
            }
        }, MoreExecutors.directExecutor());

        return resultFuture;
    }

    public void shutDown() {
        shutDownRequested = true;
        if (provider != null) {
            provider.shutDown();
        }
        Utilities.shutdownAndAwaitTermination(executorService, 2, TimeUnit.SECONDS);
    }
}
