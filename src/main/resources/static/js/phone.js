const phoneInput = document.getElementById('phoneInput');
const phoneInputPost = document.getElementById('phonePostInput');

// Общая функция для инициализации
function initPhoneInput(input) {
    let lastValue = '';

    input.addEventListener('focus', function() {
        if (!this.value || this.value === '') {
            this.value = '+7 (';
        }
    });

    input.addEventListener('input', function(e) {
        // Сохраняем позицию курсора
        let cursorPosition = this.selectionStart;

        // Сохраняем старое значение для сравнения
        const oldValue = this.value;

        // Получаем только цифры из текущего значения
        let digits = this.value.replace(/\D/g, '');

        // Если номер пустой или начинается не с 7, добавляем 7
        if (digits === '' || digits.charAt(0) !== '7') {
            digits = '7' + digits.replace(/^7/, '');
        }

        // Ограничиваем длину (10 цифр после 7)
        if (digits.length > 11) {
            digits = digits.substring(0, 11);
        }

        // Форматируем номер
        let formattedValue = '+7 (';

        // Добавляем первые 3 цифры (код оператора)
        if (digits.length > 1) {
            formattedValue += digits.substring(1, 4);
        }

        // Закрываем скобку после 3 цифр
        if (digits.length >= 4) {
            formattedValue += ') ';

            // Добавляем следующие 3 цифры
            formattedValue += digits.substring(4, 7);

            if (digits.length >= 7) {
                formattedValue += '-';
                formattedValue += digits.substring(7, 9);

                if (digits.length >= 9) {
                    formattedValue += '-';
                    formattedValue += digits.substring(9, 11);
                }
            }
        }

        // Устанавливаем новое значение
        this.value = formattedValue;

        // Корректируем позицию курсора
        if (e.inputType === 'insertText') {
            // При вводе новой цифры
            const addedChar = e.data;
            if (addedChar && /\d/.test(addedChar)) {
                // Находим позицию в отформатированной строке для новой цифры
                let digitCount = 0;
                for (let i = 0; i < formattedValue.length; i++) {
                    if (/\d/.test(formattedValue[i])) {
                        digitCount++;
                        if (digitCount === digits.length) {
                            cursorPosition = i + 1;
                            break;
                        }
                    }
                }
            }
        } else if (e.inputType === 'deleteContentBackward') {
            // При удалении
            if (cursorPosition > 0) {
                // Сначала корректируем позицию курсора на основе старого значения
                let digitCountBefore = 0;
                for (let i = 0; i < cursorPosition; i++) {
                    if (/\d/.test(oldValue[i])) {
                        digitCountBefore++;
                    }
                }

                // Находим позицию в новой строке с таким же количеством цифр перед курсором
                let newCursorPosition = 0;
                let digitCount = 0;
                for (let i = 0; i < formattedValue.length; i++) {
                    if (/\d/.test(formattedValue[i])) {
                        digitCount++;
                        if (digitCount === digitCountBefore) {
                            newCursorPosition = i + 1;
                            break;
                        }
                    }
                }

                // Если не нашли позицию (например, удалили последнюю цифру)
                if (newCursorPosition === 0) {
                    // Ищем последнюю цифру в строке
                    for (let i = formattedValue.length - 1; i >= 0; i--) {
                        if (/\d/.test(formattedValue[i])) {
                            newCursorPosition = i + 1;
                            break;
                        }
                    }
                }

                // Убедимся, что курсор не стоит перед разделителем
                while (newCursorPosition > 0 &&
                (formattedValue.charAt(newCursorPosition - 1) === ' ' ||
                    formattedValue.charAt(newCursorPosition - 1) === '-' ||
                    formattedValue.charAt(newCursorPosition - 1) === ')' ||
                    formattedValue.charAt(newCursorPosition - 1) === '(')) {
                    newCursorPosition--;
                }

                cursorPosition = newCursorPosition;
            }
        }

        // Ограничиваем позицию курсора - минимально после "+7 ("
        if (cursorPosition < 4) cursorPosition = 4;
        if (cursorPosition > formattedValue.length) cursorPosition = formattedValue.length;

        // Устанавливаем курсор
        this.setSelectionRange(cursorPosition, cursorPosition);

        // Сохраняем последнее значение
        lastValue = formattedValue;
    });

    // Обработка Backspace - более простая версия
    input.addEventListener('keydown', function(e) {
        if (e.key === 'Backspace') {
            const cursorPosition = this.selectionStart;
            const selectionEnd = this.selectionEnd;

            // Если есть выделенный текст - разрешаем стандартное поведение
            if (cursorPosition !== selectionEnd) {
                return;
            }

            // Запрещаем удаление символов "+7 ("
            if (cursorPosition <= 4 && this.value.startsWith('+7 (')) {
                e.preventDefault();
                this.setSelectionRange(4, 4);
                return;
            }

            // Если курсор стоит сразу после разделителя, перемещаем его перед ним
            if (cursorPosition > 0) {
                const charBeforeCursor = this.value.charAt(cursorPosition - 1);
                if (charBeforeCursor === '-' || charBeforeCursor === ' ' || charBeforeCursor === ')' || charBeforeCursor === '(') {
                    e.preventDefault();
                    // Ищем ближайшую цифру перед разделителем
                    let newPosition = cursorPosition - 1;
                    while (newPosition > 0 &&
                    (this.value.charAt(newPosition - 1) === ' ' ||
                        this.value.charAt(newPosition - 1) === '-' ||
                        this.value.charAt(newPosition - 1) === ')' ||
                        this.value.charAt(newPosition - 1) === '(')) {
                        newPosition--;
                    }

                    // Если мы вышли за пределы допустимой зоны
                    if (newPosition < 4) newPosition = 4;

                    this.setSelectionRange(newPosition, newPosition);

                    // Создаем новый событие Backspace
                    setTimeout(() => {
                        const newEvent = new KeyboardEvent('keydown', {
                            key: 'Backspace',
                            code: 'Backspace',
                            bubbles: true
                        });
                        this.dispatchEvent(newEvent);
                    }, 10);
                }
            }
        }

        // Запрещаем ввод нецифр, кроме управляющих клавиш
        if (e.key.length === 1 && !/\d/.test(e.key) &&
            !['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Enter'].includes(e.key)) {
            e.preventDefault();
        }
    });

    // Обработка потери фокуса - проверяем корректность
    input.addEventListener('blur', function() {
        const digits = this.value.replace(/\D/g, '');
        if (digits.length <= 1) {   // только "7" или пусто
            this.value = '';
        }
        if (digits.length < 11 && digits.length > 1) {
            console.log('Номер неполный');
        }
    });

}

// Инициализируем оба поля
if (phoneInput) initPhoneInput(phoneInput);
if (phoneInputPost) initPhoneInput(phoneInputPost);