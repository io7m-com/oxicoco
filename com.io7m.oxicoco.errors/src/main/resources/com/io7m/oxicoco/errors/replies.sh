#!/bin/sh

IFS="
"

cat <<EOF
public enum OxIRCReply
{
EOF

for LINE in $(cat replies.txt)
do
  ID=$(echo $LINE | awk -F, '{print $1}')
  CODE=$(echo $LINE | awk -F, '{print $2}')
  TEXT=$(echo $LINE | awk -F, '{print $NF}' | sed 's/"//g')

  cat <<EOF

  /**
   * ${CODE}
   */

  ${CODE} (${ID}),
EOF
done

cat <<EOF
  ;

  private final int code;

  public int code()
  {
    return this.code;
  }

  OxIRCReply(int inCode)
  {
    this.code = inCode;
  }
}
EOF
