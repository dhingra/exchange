package io.bisq.core.btc.blockchain.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.bisq.common.app.Log;
import io.bisq.network.http.HttpClient;
import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

public class BlockTrailProvider extends BlockchainTxProvider {
    private static final Logger log = LoggerFactory.getLogger(BlockTrailProvider.class);

    @Inject
    public BlockTrailProvider(HttpClient httpClient) {
        super(httpClient, "https://www.blocktrail.com/BTC/json/blockchain/tx/");
    }

    @Override
    public Coin getFee(String transactionId) throws IOException {
        Log.traceCall("transactionId=" + transactionId);
        try {
            JsonObject asJsonObject = new JsonParser()
                    .parse(httpClient.requestWithGET(transactionId, "User-Agent", ""))
                    .getAsJsonObject();
            return Coin.valueOf(asJsonObject
                    .get("fee")
                    .getAsLong());
        } catch (IOException e) {
            log.debug("Error at requesting transaction data from block explorer " + httpClient + "\n" +
                    "Error =" + e.getMessage());
            throw e;
        }
    }

    @Override
    public String toString() {
        return "BlockTrailProvider{" +
                '}';
    }
}
