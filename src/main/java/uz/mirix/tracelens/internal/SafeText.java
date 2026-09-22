package uz.mirix.tracelens.internal;
final class SafeText {private SafeText(){} static String singleLine(String value,int maxLength){if(value==null)return "";String safe=value.replace('\r',' ').replace('\n',' ').replace('\t',' ').trim();if(safe.length()<=maxLength)return safe;return safe.substring(0,Math.max(0,maxLength-1))+"…";}}
