package com.example.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;

public class Common {
    public static Pairing pairing = PairingFactory.getPairing("a.properties");

    public final static String g1String = "LzsPlRBJW971TyZZoIOOojjPZMI2IfonqgV1GD8mVCqScD0cC0MTMSNimm4gWhcmomGZk2qwWwr2uJqD7U/GCpGT/9uP3DzBW0A4X/bb2KFaH/7li5UNFxM5jx0P91fNwoKEi9uQkM3TfaspNatF22eDzAO0XSR1llMDIjWREGI=";

    // public static Pairing pairing =
    // PairingFactory.getPairing("bls12-381.properties");

    // public static String g1String =
    // "FkRPm1FY9TfTXzM9ARertvm/hB4M/D0HS6UChQE7IR61zuX+YYC/YQ==";

    public final static ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier("1.2.156.10197.1.301.1");
}
