export function parseJSONC(value: unknown): unknown {
    const characters = String(value).split("");
    for (const comments of [true, false]) {
        let quoted = false;
        for (let index = 0; index < characters.length; index++) {
            const character = characters[index];
            if (quoted) {
                if (character === "\\") {
                    index++;
                } else if (character === '"') {
                    quoted = false;
                }
                continue;
            }
            if (character === '"') {
                quoted = true;
                continue;
            }
            if (comments && character === "/" && characters[index + 1] === "/") {
                while (
                    index < characters.length &&
                    characters[index] !== "\r" &&
                    characters[index] !== "\n"
                ) {
                    characters[index++] = " ";
                }
                index--;
            } else if (comments && character === "/" && characters[index + 1] === "*") {
                let end = index + 2;
                while (
                    end + 1 < characters.length &&
                    !(characters[end] === "*" && characters[end + 1] === "/")
                ) {
                    end++;
                }
                if (end + 1 >= characters.length) {
                    throw new SyntaxError("Unterminated JSONC block comment");
                }
                for (; index <= end + 1; index++) {
                    if (characters[index] !== "\r" && characters[index] !== "\n") {
                        characters[index] = " ";
                    }
                }
                index--;
            } else if (!comments && character === ",") {
                let previous = index - 1;
                while (previous >= 0 && characters[previous].trim() === "") {
                    previous--;
                }
                let next = index + 1;
                while (next < characters.length && characters[next].trim() === "") {
                    next++;
                }
                if (
                    previous >= 0 &&
                    !"[{,:".includes(characters[previous]) &&
                    (characters[next] === "]" || characters[next] === "}")
                ) {
                    for (; index < next; index++) {
                        if (characters[index] !== "\r" && characters[index] !== "\n") {
                            characters[index] = " ";
                        }
                    }
                    index--;
                }
            }
        }
    }
    return JSON.parse(characters.join(""));
}
