package org.ekstep.ep.samza.converter.domain;

import com.google.gson.annotations.SerializedName;
import org.ekstep.ep.samza.reader.NullableValue;
import org.ekstep.ep.samza.reader.Telemetry;
import org.ekstep.ep.samza.reader.TelemetryReaderException;

import java.util.*;

public class Context {
    private String channel = "";
    private String env = "";
    private String sid = "";
    private String did = "";

    @SerializedName("pdata")
    private PData pData;

    @SerializedName("cdata")
    private ArrayList<CData> cData = new ArrayList<>();

    private Rollup rollUp;

    public Context() {
    }

    public Context(Telemetry reader) throws TelemetryReaderException {
        channel = reader.<String>read("channel").valueOrDefault("in.ekstep");
        if ("".equals(channel.trim())) {
            channel = "in.ekstep";
        }
        pData = new PData(reader);

        String eid = reader.mustReadValue("eid");
        String env = reader.<String>read("edata.eks.env").valueOrDefault("");
        if (!env.equals("")) {
            this.env = env;
        } else if (eid.startsWith("OE_")) {
            this.env = "ContentPlayer";
        } else if (eid.startsWith("GE_")) {
            this.env = "Genie";
        } else if (eid.startsWith("CE_")) {
            this.env = "ContentEditor";
        }

        // sid is a mandatory field. but it can come in two possible paths
        // - sid (at the envelope)
        // - context.sid (CE and CP events)
        NullableValue<String> sid = reader.read("sid");
        if (sid.isNull()) {
            // sid in envelope is null. so it might be in context.sid
            this.sid = reader.<String>read("context.sid").valueOrDefault("");
        } else {
            this.sid = sid.value();
        }

        did = reader.<String>read("did").valueOrDefault("");

        List v2CData = reader.<List>read("cdata").valueOrDefault(new ArrayList());
        for (Object item : v2CData) {
            Map<String, Object> m = (Map<String, Object>) item;
            cData.add(new CData(m));
        }

        // etags.partner should come in the cdata
        if( !reader.<List<String>>read("etags.partner").isNull() ){
            List<String> partnerETags = reader.<List<String>>read("etags.partner").valueOrDefault(new ArrayList<>());
            for (String eTag: partnerETags) {
                cData.add(new CData("partner", eTag));
            }
        } else {
            List<Map<String,Object>> tags = reader.<List<Map<String,Object>>>read("tags").value();
            if( tags != null && !tags.isEmpty()) {
                for( int i=0; i < tags.size(); i++) {
                    if( tags.get(i) instanceof Map ) {
                        Map partnerTags = tags.get(i);
                        if (partnerTags != null && partnerTags.containsKey("partnerid") ) {
                            List<String> pTags = (List<String>) partnerTags.get("partnerid");
                            for (String pTag : pTags) {
                                cData.add(new CData("partner", pTag));
                            }
                        }
                    }
                }
            }
        }
    }

    public String getChannel() {
        return channel;
    }

    public String getEnv() {
        return env;
    }

    public String getSid() {
        return sid;
    }

    public String getDid() {
        return did;
    }

    public PData getpData() {
        return pData;
    }

    public Rollup getRollUp() {
        return rollUp;
    }

    public void setRollUp(Rollup rollUp) {
        this.rollUp = rollUp;
    }

    public List<CData> getCData() {
        return cData;
    }
}
