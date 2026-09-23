document.querySelectorAll("[data-quiz]").forEach((quiz) => {
  const feedback = quiz.querySelector(".feedback");
  quiz.querySelectorAll("button").forEach((button) => {
    button.addEventListener("click", () => {
      feedback.textContent = button.dataset.correct === "true"
        ? "对。现在用自己的话解释一次。"
        : "再想想：题目问的是协议动作，不是业务消息。";
    });
  });
});
