<html>
<body>
	<h1>Model File Upload</h1>

	<form action="/SearchEngine/api/ltr/model/upload" method="post" enctype="multipart/form-data">

       <p>
        Model name: <input type="text" name="model_name">
       </p>
       <p>
       Model type: <input type="text" name="model_type">
       </p>
	   <p>
		Select a file : <input type="file" name="file" size="45" />
	   </p>

	   <input type="submit" value="Upload" />
	</form>

</body>
</html>