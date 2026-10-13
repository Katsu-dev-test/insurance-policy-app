const input = document.getElementById("policyNumber");
const button = document.getElementById("searchButton");
const warning = document.getElementById("warning");
const error = document.getElementById("error");
const result = document.getElementById("result");

function reset() {
  warning.hidden = true;
  error.hidden = true;
  result.hidden = true;
}

async function search() {
  reset();
  const policyNumber = input.value.trim();
  try {
    const res = await fetch("/api/policies/" + encodeURIComponent(policyNumber));
    if (res.status === 404) {
      error.textContent = "該当する契約が見つかりません。";
      error.hidden = false;
      return;
    }
    if (!res.ok) {
      error.textContent = "契約番号の形式が正しくありません。";
      error.hidden = false;
      return;
    }
    const policy = await res.json();
    // textContent のみを使い、API の値を HTML として解釈しない
    document.getElementById("resPolicyNumber").textContent = policy.policyNumber;
    document.getElementById("resHolderName").textContent = policy.policyHolderName;
    document.getElementById("resExpiryDate").textContent = policy.expiryDate;
    result.hidden = false;
    warning.hidden = !policy.expiringSoon;
  } catch (e) {
    error.textContent = "照会に失敗しました。時間をおいて再度お試しください。";
    error.hidden = false;
  }
}

button.addEventListener("click", search);
input.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    search();
  }
});
