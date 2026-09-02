---
title: Ghi chú phát hành
layout: page
nav_order: 5
has_toc: false
lang: vi
permalink: /release-notes.html
machine-translated: true
---

# Ghi chú phát hành

Soundscape 2.0 là một bản phát hành lớn và hiện đang trong giai đoạn beta khép kín. Thay đổi nổi bật
nhất là giờ đây Soundscape có điều gì đó hữu ích để nói khi bạn đi ô tô, xe buýt hoặc tàu hỏa, chứ
không chỉ khi đi bộ. Bên cạnh đó là rất nhiều công việc nhỏ hơn về cách mô tả địa điểm, hai mươi ngôn
ngữ mới và một danh sách dài các bản sửa lỗi.

Ghi chú của các phiên bản cũ hơn nằm ở trang
[Ghi chú phát hành cho 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Có gì mới trong 2.0

* **Thông báo khi đi ô tô, xe buýt hoặc tàu hỏa.** Soundscape nhận ra bạn đang di chuyển với tốc độ
  cao và mô tả hành trình của bạn thay vì môi trường ngay xung quanh.
* **Báo khi bạn vượt qua sông nước và đường sắt.** Sông, kênh đào, vịnh và tuyến đường sắt được thông
  báo khi bạn đi qua, dù đang đi bộ hay đang di chuyển.
* **Địa chỉ và tên địa điểm tốt hơn.** Những nơi không có địa chỉ riêng giờ được gắn với con phố và
  khu vực nơi chúng tọa lạc, số nhà được khớp với đúng bên đường, và các điểm dừng xe buýt ở Vương
  quốc Anh dùng tên chính thức của chúng.
* **Hai mươi ngôn ngữ mới**, nâng tổng số lên 46. Trang tài liệu này cũng đã được dịch.
* **Thức dậy khi rời đi.** Chế độ ngủ giờ có thể đánh thức Soundscape khi bạn rời khỏi nơi mà bạn đã
  cho nó ngủ.
* **Khoảng cách ngắn gọn, tự nhiên hơn**, dùng đơn vị lớn hơn khi bạn di chuyển nhanh.
* **Lối thoát nhanh hơn.** *Thoát Soundscape* giờ nằm ở đầu menu chính.
* **Cải tiến bản đồ ngoại tuyến**, bao gồm cập nhật tại chỗ một bản đồ đã tải và một bản đồ các khu
  vực sẵn có trên trang này.
* **Rất nhiều việc về khả năng tiếp cận** với TalkBack, đặc biệt quanh các màn hình khởi đầu.
* **Rất nhiều bản sửa lỗi treo và ổn định.**

Hai thứ đã bị **loại bỏ** trong 2.0: điều khiển bằng giọng nói và menu ngôn ngữ bên trong ứng dụng.
Xem [Các tính năng đã bị loại bỏ](#things-that-have-been-removed) bên dưới để biết nên làm gì thay
thế.

---

## Chi tiết hơn

### Đi ô tô, xe buýt hoặc tàu hỏa

Đây là tính năng mới lớn nhất dành cho người dùng hiện tại. Trước đây Soundscape gần như không có gì
để nói ngay khi bạn lên xe: nó vẫn tiếp tục mô tả môi trường ngay xung quanh, mà ở tốc độ cao thì
điều đó biến thành một dòng những thứ bạn đã đi qua từ lâu.

Giờ đây Soundscape nhận thấy bạn đang di chuyển nhanh hơn tốc độ đi bộ và thay đổi những gì nó nói.
Không có gì phải bật, và mọi thứ tự trở lại bình thường ngay khi bạn chậm lại hoặc xuống xe đi bộ.

Trên đường đi bạn sẽ nghe:

* **Bạn đang ở đâu**, thỉnh thoảng — con đường bạn đang đi và hướng di chuyển, ví dụ «Đang đi về phía
  bắc trên M8». Những con đường có số hiệu được thông báo bằng số hiệu, và Soundscape không lặp lại
  cùng một con đường mỗi lần tên phố thay đổi.
* **Các thị trấn và làng mạc** bạn đang tiến tới, kèm khoảng cách, cũng như những nơi bạn đang rời xa
  hoặc chỉ đi ngang qua.
* **Các nút giao và lối ra đường cao tốc** khi bạn tới nơi.
* **Những mốc lớn** khi bạn đi ngang, chẳng hạn công viên, bệnh viện, sân vận động và trung tâm mua
  sắm.
* **Các điểm dừng xe buýt, tàu điện và ga tàu** khi bạn đi ngang. Soundscape chỉ nhắc những điểm dừng
  ở phía đường của bạn, vì những điểm bên kia phục vụ hướng ngược lại.
* **Sông, kênh đào và đường sắt bạn vượt qua.**
* **Đường hầm**, điều này chủ yếu giải thích vì sao Soundscape sắp im lặng — bên trong không có tín
  hiệu GPS.

Trên **tàu hỏa**, Soundscape hiểu rằng bạn đang ở trên đường sắt chứ không phải đường bộ, và cho bạn
biết bạn đang đi ngang những nơi nào cùng quãng đường đã đi kể từ ga gần nhất. Việc xác định điều này
khó hơn nghe tưởng, vì đường cao tốc và tuyến đường sắt thường được xây song song hàng cây số, nên một
phần đáng kể công sức trong bản phát hành này dành cho việc không nhầm cái này với cái kia.

Các thông báo thông thường dành cho người đi bộ — cửa hàng gần đó, vạch sang đường và tương tự — bị
giữ lại có chủ đích khi bạn đang di chuyển, và khoảng cách mà mọi thứ được thông báo đã được nới rộng
đáng kể, để bạn biết về một thứ gì đó trước khi đi qua nó.

### Vượt qua sông nước và đường sắt

Soundscape giờ cho bạn biết khi bạn vượt qua một con sông, kênh đào, vịnh, vũng hay tuyến đường sắt.
Điều này hoạt động cả khi đi bộ lẫn khi di chuyển, và bao gồm cả đi bên dưới lẫn đi bên trên, nên cầu
bộ hành và hầm chui đều được mô tả.

### Địa chỉ và tên địa điểm tốt hơn

Rất nhiều công sức đã bỏ ra để Soundscape mô tả địa điểm theo cách một con người sẽ mô tả:

* Những nơi không có địa chỉ riêng giờ được mô tả bằng con phố và khu vực nơi chúng tọa lạc, thay vì
  cứ mơ hồ.
* Số nhà được khớp với đúng bên đường. Trước đây một địa chỉ có thể được báo từ vỉa hè đối diện.
* Địa chỉ của một địa điểm không còn lặp lại chính tên của địa điểm đó.
* Các điểm dừng xe buýt ở Vương quốc Anh dùng tên chính thức của giao thông công cộng, thường là tên
  trên bảng giờ chạy và trên biển tại điểm dừng.
* Những lối đi bộ không tên chạy dọc một con sông hay kênh đào giờ được đặt tên theo dòng nước mà
  chúng men theo.
* Lối đi và con đường không có tên được mô tả hợp lý hơn, và những từ dùng cho chúng đã được dịch tử
  tế thay vì hiện ra bằng tiếng Anh.

### Ngôn ngữ

Hai mươi ngôn ngữ mới đã được thêm vào 2.0: tiếng Ả Rập, Bengal, Bulgaria, Catalan, Croatia, Séc,
Hausa, Hungary, Indonesia, Hàn, Marathi, Serbia, Slovakia, Slovenia, Swahili, Tamil, Telugu, Thái,
Urdu và Việt. Tất cả những ngôn ngữ này đều đang ở giai đoạn alpha, và chúng tôi rất mong nhận được
phản hồi về độ chính xác của chúng. Tổng cộng Soundscape hiện có sẵn bằng 46 ngôn ngữ, và trang tài
liệu này cũng đã được dịch.

Tiếng Ả Rập Ai Cập đã được gộp vào tiếng Ả Rập, còn tiếng Luganda đã được rút lại, vì cả hai đều không
có đủ văn bản dịch để hữu ích.

Bản dịch là công sức của cộng đồng và chúng tôi hoan nghênh sự giúp đỡ của bạn, hoặc các sửa chữa ở
những chỗ đọc lên chưa ổn. Mọi văn bản đều có thể được cải thiện tại
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Chế độ ngủ

Chế độ ngủ đã có thêm **thức dậy khi rời đi**. Khi bạn cho Soundscape ngủ, bạn có thể yêu cầu nó thức
dậy ngay khi bạn rời khỏi khu vực, điều này hữu ích khi bạn tới nơi nào đó và muốn yên tĩnh cho tới
lần khởi hành kế tiếp.

### Khoảng cách và lời nói

Các khoảng cách được đọc lên đã ngắn gọn và tự nhiên hơn, và Soundscape giờ chuyển sang đơn vị lớn hơn
khi bạn di chuyển nhanh — dặm hoặc ki-lô-mét thay vì một phép đếm dài bằng foot hay mét. Mỗi ngôn ngữ
tự quyết định cách đọc một khoảng cách lẻ, điều trước đây bị ép vào một khuôn mẫu kiểu tiếng Anh.

### Bản đồ ngoại tuyến

Bản đồ ngoại tuyến xuất hiện từ 1.0 và liên tục được cải thiện:

* Một bản đồ đã tải giờ có thể được cập nhật tại chỗ khi có phiên bản mới hơn, từ màn hình chi tiết
  của phần trích xuất.
* Những bản đồ không dùng được — chẳng hạn một tệp tải về bị hỏng — giờ được đánh dấu rõ ràng thay vì
  âm thầm thất bại.
* Việc tải xuống đáng tin cậy hơn, và màn hình cho thấy điều gì đang diễn ra trong khi lấy danh sách
  bản đồ sẵn có, thay vì một vòng quay tải toàn màn hình.
* Một lượt tải đã xong chỉ hiển thị là xong khi nó thực sự sẵn sàng để dùng.
* Trên trang này có
  [bản đồ các khu vực sẵn có]({{ "/users/help-offline-map-extracts.html" | relative_url }}).

### Khả năng tiếp cận

Rất nhiều công sức đã dành cho hành vi của trình đọc màn hình, đặc biệt ở các màn hình khởi đầu nơi
tiêu điểm trước đây nhảy sai chỗ. Các cải tiến khác gồm đọc kích thước tệp và số thập phân tốt hơn,
gợi ý dạng «chạm hai lần để...» đúng trong những ngôn ngữ đặt động từ ở cuối, và gợi ý hợp lý ở những
chỗ vốn chẳng có gợi ý nào.

### Menu và điều hướng

* **Thoát Soundscape** giờ là mục đầu tiên trong menu chính thay vì nằm đâu đó phía dưới.
* Menu chính không còn để lộ một dải màn hình ở một bên, thứ từng tạo cho người dùng trình đọc màn
  hình một vùng chạm phụ gây bối rối.
* Cử chỉ quay lại của hệ thống không còn bỏ qua một cấp khi bạn duyệt các danh mục trong «Địa điểm gần
  đây».
* *Hướng dẫn bằng âm thanh* đã được đổi tên thành **hướng dẫn có dẫn dắt**.
* Phần cài đặt đã được sắp xếp lại, và *Đặt lại về mặc định* giờ xóa mọi thứ đúng cách.

### Độ ổn định

2.0 bao gồm một danh sách dài các lỗi treo và đứng máy đã được sửa, trong đó có ứng dụng đứng ở màn
hình khởi động, đứng khi đặt lại cài đặt, lỗi khi bản đồ tải về bị hỏng, lỗi khi mở chi tiết tuyến
đường từ màn hình chính, lỗi khi đổi ngôn ngữ, cùng một số vấn đề được báo cáo tự động qua Play Store.
Hành vi liên quan tới pin và khởi động cũng được làm vững chắc hơn trên những điện thoại đóng ứng dụng
nền một cách quyết liệt.

### Các tính năng đã bị loại bỏ
{: #things-that-have-been-removed }

* **Điều khiển bằng giọng nói** đã bị loại bỏ. Nó chưa bao giờ hoạt động đủ tin cậy để giữ lại, và các
  nút điều khiển media trên tai nghe đáp ứng phần lớn cùng nhu cầu — xem
  [Trợ giúp về việc dùng nút điều khiển media]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Menu ngôn ngữ bên trong ứng dụng** đã biến mất. Soundscape giờ theo ngôn ngữ bạn đặt cho điện
  thoại, điều mà phần lớn mọi người vẫn mong đợi. Để thay đổi, hãy đổi ngôn ngữ của điện thoại, hoặc
  đặt ngôn ngữ riêng cho từng ứng dụng trong cài đặt điện thoại nếu máy hỗ trợ.

## Cách báo cho chúng tôi về sự cố

Nếu có gì đó không ổn, chúng tôi rất muốn biết. Hãy gửi thư tới Help Desk tại
<soundscapeAndroid@scottishtecharmy.support>, hoặc hỏi trên Slack nếu bạn là thành viên STA.

Nếu một thông báo bị sai hoặc không xuất hiện, một bản ghi hành trình của bạn giúp chúng tôi rất
nhiều — chúng tôi có thể phát lại và thấy chính xác Soundscape đã dựa trên dữ liệu nào. Hướng dẫn nằm
ở phần
[Cung cấp bản ghi vị trí để gỡ lỗi]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Đôi lời về iPhone

Tất cả những điều trên là về ứng dụng Android, nhưng cũng đáng biết phần công sức còn lại của bản phát
hành này đã đi đâu. Soundscape giờ chạy cả trên iPhone, và cả hai ứng dụng đều được xây từ cùng một mã
nguồn dùng chung — cùng màn hình, cùng cách diễn đạt và cùng thông báo. Nhờ vậy một tính năng mới như
các thông báo hành trình ở trên đến với cả hai cùng lúc thay vì phải viết hai lần. Chính nền tảng chung
đó giải thích vì sao 2.0 mất nhiều thời gian đến thế, và cũng chính nó sẽ giúp các bản phát hành sau
tới cả hai nền tảng nhanh hơn. Ứng dụng iPhone hiện có qua TestFlight theo lời mời: hãy hỏi trên Slack
nếu bạn là thành viên STA, hoặc gửi thư tới Help Desk.
